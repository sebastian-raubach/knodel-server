package jhi.knodel.data;

import java.sql.*;

import jhi.knodel.resource.*;

/**
 * @author Sebastian Raubach
 */
public class DatasourceDAO
{
	public KnodelDatasourceList getAll()
	{
		KnodelDatasourceList result = new KnodelDatasourceList();

		try (Connection con = Database.INSTANCE.getMySQLDataSource().getConnection();
			 PreparedStatement stmt = con.prepareStatement("SELECT * FROM datasources");
			 ResultSet rs = stmt.executeQuery())
		{
			while (rs.next())
			{
				result.add(Parser.Inst.get().parse(rs));
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		return result;
	}

	public void updateSize(KnodelDatasource datasource)
	{
		try (Connection con = Database.INSTANCE.getMySQLDataSource().getConnection();
			 PreparedStatement stmt = DatabaseUtils.updateByIdLongStatement(con, "UPDATE datasources SET size = ? WHERE id = ?", datasource.getId(), datasource.getSize()))
		{
			stmt.executeUpdate();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}


	public static class Parser extends DatabaseObjectParser<KnodelDatasource>
	{
		public static final class Inst
		{
			/**
			 * {@link InstanceHolder} is loaded on the first execution of {@link Inst#get()} or the first access to {@link InstanceHolder#INSTANCE},
			 * not before.
			 * <p/>
			 * This solution (<a href= "http://en.wikipedia.org/wiki/Initialization_on_demand_holder_idiom" >Initialization-on-demand holder
			 * idiom</a>) is thread-safe without requiring special language constructs (i.e. <code>volatile</code> or <code>synchronized</code>).
			 *
			 * @author Sebastian Raubach
			 */
			private static final class InstanceHolder
			{
				private static final Parser INSTANCE = new Parser();
			}

			public static Parser get()
			{
				return InstanceHolder.INSTANCE;
			}
		}

		@Override
		public KnodelDatasource parse(ResultSet rs) throws SQLException
		{
			return new KnodelDatasource(rs.getInt(KnodelDatasource.FIELD_ID), rs.getTimestamp(KnodelDatasource.FIELD_CREATED_ON), rs.getTimestamp(KnodelDatasource.FIELD_UPDATED_ON))
					.setName(rs.getString(KnodelDatasource.FIELD_NAME))
					.setDescription(rs.getString(KnodelDatasource.FIELD_DESCRIPTION))
					.setVersionNumber(rs.getInt(KnodelDatasource.FIELD_VERSION_NUMBER))
					.setDataProvider(rs.getString(KnodelDatasource.FIELD_DATA_PROVIDER))
					.setContact(rs.getString(KnodelDatasource.FIELD_CONTACT))
					.setIcon(rs.getString(KnodelDatasource.FIELD_ICON))
					.setSize(rs.getLong(KnodelDatasource.FiELD_SIZE));
		}
	}
}