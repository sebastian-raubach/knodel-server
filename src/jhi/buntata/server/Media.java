/*
 * Copyright 2018 Information & Computational Sciences, The James Hutton Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jhi.buntata.server;

import net.coobird.thumbnailator.*;

import org.restlet.data.*;
import org.restlet.data.Status;
import org.restlet.representation.*;
import org.restlet.resource.*;

import java.io.*;
import java.util.logging.*;

import javax.servlet.*;

import jhi.buntata.data.*;
import jhi.buntata.resource.*;

/**
 * {@link ServerResource} handling {@link Media} requests.
 *
 * @author Sebastian Raubach
 */
public class Media extends ServerResource
{
	private static String dataDir;

	public static final String PARAM_SIZE = "small";

	private final MediaDAO dao   = new MediaDAO();
	private       Long     id    = null;
	private       boolean  small = false;

	@Override
	public void doInit()
	{
		super.doInit();

		try
		{
			this.id = Long.parseLong(getRequestAttributes().get("id").toString());
		}
		catch (NullPointerException | NumberFormatException e)
		{
		}

		try
		{
			this.small = Boolean.parseBoolean(getQueryValue(PARAM_SIZE));
		}
		catch (NullPointerException e)
		{
		}
	}

	@Delete("json")
	public boolean deleteJson()
	{
		if (id == null)
		{
			throw new ResourceException(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
		}
		else
		{
			BuntataMedia media = dao.get(id);

			if (media != null)
			{
				dao.delete(media.getId());
				return true;
			}
			else
			{
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
			}
		}
	}

	@Post("json")
	public Long postJson(BuntataMedia media)
	{
		if (id != null)
		{
			throw new ResourceException(400);
		}
		else
		{
			return dao.add(media);
		}
	}

	@Put("json")
	public Long putJson(BuntataMedia media)
	{
		if (id == null)
		{
			throw new ResourceException(422);
		}
		else
		{
			BuntataMedia md = dao.get(id);

			if (md != null)
			{
				throw new ResourceException(409);
			}
			else
			{
				return dao.add(media);
			}
		}
	}

	@Get()
	public Representation getImage()
	{
		BuntataMedia media = dao.get(id);

		FileRepresentation representation = null;

		if (media != null)
		{
			File file = new File(dataDir, media.getInternalLink());

			// Check if the image exists
			if (file.exists() && file.isFile() && media.getMediaTypeId() == 1)
			{
				try
				{
					MediaType type;

					if (file.getName().toLowerCase().endsWith(".jpg"))
						type = MediaType.IMAGE_JPEG;
					else if (file.getName().toLowerCase().endsWith(".png"))
						type = MediaType.IMAGE_PNG;
					else
						type = MediaType.IMAGE_ALL;

					if (small)
					{
						ServletContext servlet = (ServletContext) getContext().getAttributes().get("org.restlet.ext.servlet.ServletContext");
						String version = servlet.getInitParameter("version");
						File folder = new File(System.getProperty("java.io.tmpdir"), "buntata-thumbnails" + "-" + version);
						folder.mkdirs();

						String extension = type == MediaType.IMAGE_PNG ? ".png" : ".jpg";

						File target = new File(folder, media.getId() + "-small" + extension);

						// Delete the thumbnail if it's older than the source image
						if (target.lastModified() < file.lastModified())
							target.delete();

						// If it exists, fine, just return it
						if (target.exists())
						{
							file = target;
						}
						// If not, create a new thumbnail
						else
						{
							Thumbnails.of(file)
									  .height(400)
									  .keepAspectRatio(true)
									  .toFile(target);

							file = target;
						}
					}

					representation = new FileRepresentation(file, type);
					representation.setSize(file.length());
					representation.setDisposition(new Disposition(Disposition.TYPE_ATTACHMENT));
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				Logger.getLogger("").log(Level.WARNING, "File not found: " + file.getAbsolutePath());
			}
		}
		else
		{
			Logger.getLogger("").log(Level.WARNING, "Media not found: " + id);
		}

		return representation;
	}

	public static void setDataDir(String dataDir)
	{
		Media.dataDir = dataDir;
	}
}
