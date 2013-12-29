package wmg.datagrabber.google.youtubegrabber.cmdline;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

public class DMLClass {
	Connection connection = null;
	PreparedStatement pStmt = null;
	Boolean autoCommit = false;
	Integer countBatch = 0;
	String sqlStatement = null;
	
	public Integer getCountBatch() {
		return countBatch;
	}

	public void setCountBatch(Integer countBatch) {
		this.countBatch = countBatch;
	}

	public void bumpCountBatch() {
		this.countBatch++; 
	}
	
	
	public DMLClass(Connection connection) {	
		this.connection = connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	public PreparedStatement getpStmt() {
		return pStmt;
	}

	public void setpStmt(PreparedStatement pStmt) {
		this.pStmt = pStmt;
	}

	public void setSQLStatement (String sqlStmt) throws SQLException {
		sqlStatement = sqlStmt;
		setpStmt(connection.prepareStatement(sqlStmt));
	}
	
	public Boolean getAutoCommit() {
		return autoCommit;
	}

	public void setAutoCommit(Boolean aCommit) throws SQLException {
		if ( connection != null ) {
			autoCommit = aCommit;
			connection.setAutoCommit(aCommit);
		}		
	}

	public void setPreparedStatement( String sqlStatement) {
		if ( connection != null ) { 
			try {
				pStmt = connection.prepareStatement(sqlStatement);
			} catch (SQLException e) {
				e.printStackTrace();
			}			
		}		
	}
	
	public void setInteger( int position, int value) throws SQLException { 
		if ( pStmt != null ) { 
			pStmt.setInt(position, value);
		}
	}
	
	public void setString( int position, String value) throws SQLException { 
		if ( pStmt != null ) { 
			pStmt.setString(position, value);
		}
	}	

	public void setDate( int position, Date value) throws SQLException { 
		if ( pStmt != null ) { 
			pStmt.setDate(position, (java.sql.Date) value);
		}
	}		
	
	public void setBoolean( int position, Boolean value) throws SQLException { 
		if ( pStmt != null ) { 
			pStmt.setBoolean(position, value);
		}
	}		

	public void addStatementToBatch() throws SQLException {
		if ( pStmt != null ) { 
			pStmt.addBatch();
		}			
	}

	public void executeBatch() throws SQLException {
		if ( pStmt != null ) { 
			pStmt.executeBatch();
		}
		setCountBatch(0);
	}	
	
	public void closePreparedStatement() throws SQLException {
		if ( pStmt != null && connection != null) { 
			pStmt.close();
		}			
	}			
	
	public void commitStatements() throws SQLException {
		if ( pStmt != null && connection != null) { 
			connection.commit();
		}			
	}
}
