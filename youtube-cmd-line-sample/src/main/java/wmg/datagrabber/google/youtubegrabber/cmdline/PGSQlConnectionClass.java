package wmg.datagrabber.google.youtubegrabber.cmdline;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PGSQlConnectionClass {
	Connection connection = null;
	private String _host;
	private String _port;
	private String _dbname;
	private String _username;
	private String _password;


	public PGSQlConnectionClass(String host, String port, String dbname,
			String username, String password) {
		super();
		this._host = host;
		this._port = port;
		this._dbname = dbname;
		this._username = username;
		this._password = password;
	}


	public Connection getPGSQLConnection() {
		if ( isValidConnection()) return connection;
		try {
			Class.forName("org.postgresql.Driver");
			connection = DriverManager.getConnection(
										makeDBURL(_host, _port, _dbname), 
										_username,
										_password);
		} catch (ClassNotFoundException e) {
			System.out.println("ERROR: PostgreSQL JDBC Driver not found in the library path.");
		} catch (SQLException e) {
			System.out.println("Connection Failed! Check output console" + e.getMessage());
			e.printStackTrace();
		}
		return connection;
	}
	
	private static String makeDBURL(String host, String port, String dbname) {
		String dburl = "jdbc:postgresql://" + host + ":" + port + "/" + dbname;
		//System.out.println(" The dburl " + dburl );
		return dburl;
	}
	
	public void closeConnection() { 
		if ( isValidConnection()) {
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public Boolean isValidConnection() { 
		try {
			if ( connection.isValid(2) && !connection.isClosed() ) return true;
		} catch (SQLException e1) {
			e1.printStackTrace();
		}		
		return false;
	}	
}
