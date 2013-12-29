/*
 * Copyright (c) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package wmg.datagrabber.google.youtubegrabber.cmdline;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeRequest;
import com.google.api.services.youtube.YouTube.Channels.List;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.Activity;
import com.google.api.services.youtube.model.ActivityContentDetails;
//import com.google.api.services.youtube.model.ActivityContentDetails.Bulletin;
import com.google.api.services.youtube.model.ActivitySnippet;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.client.http.javanet.NetHttpTransport;

import com.google.api.services.youtubeAnalytics.YouTubeAnalytics;
//import com.google.api.services.youtubeAnalytics.YoutubeAnalytics;
import com.google.api.services.youtubeAnalytics.model.ResultTable;
import com.google.api.services.youtubeAnalytics.model.ResultTable.ColumnHeaders;
//import com.google.common.collect.Lists;
import java.math.BigDecimal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

public class YouTubeGrabber {

	/**
	 * Be sure to specify the name of your application. If the application name
	 * is {@code null} or blank, the application will log a warning. Suggested
	 * format is "MyCompany-ProductName/1.0".
	 */

	private Credential credential = null;
	//private	Connection connection = null;
	private static final String APPLICATION_NAME = "WMG";
	private static final String ON_BEHALF_OF_OWNER = "WMG";
	/** Directory to store user credentials. */
	private static final java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"), ".store/youtube_sample");
	private static FileDataStoreFactory dataStoreFactory;

	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	private static HttpTransport httpTransport;

	@SuppressWarnings("unused")
	private YouTube youtube;
	
	private YouTubeAnalytics analytics;

	private Properties props = new Properties();
	private String _host;
	private String _port;
	private String _dbname;
	private String _username;
	private String _password;

	public YouTubeGrabber() {
		loadProperties();
	}

	private void loadProperties() {
		//Run -> Run configurations, select project, second tab: “Arguments”. Top box is for your program, 
		//bottom box is for VM arguments, e.g. -DparamFile=value.
		String propertiesFile = System.getProperty("paramFile");		
		InputStream is = null;

		try {
			is = new FileInputStream(new File(propertiesFile));
			props.load(is);			
		} catch (FileNotFoundException e) {
			System.out.println(" Exception " + e.getMessage());
			e.printStackTrace();
		} catch (IOException ioe) {
			System.out.println(" Exception " + ioe.getMessage());
			ioe.printStackTrace();
		}
		_host = props.getProperty("host");
		_port = props.getProperty("port");
		_dbname = props.getProperty("dbname");
		_username = props.getProperty("username");
		_password = props.getProperty("password");
		System.out.println(" HOST: " + _host );
	}
	
	/** Authorizes the installed application to access user's protected data. */
	private Boolean authorize() {
		// load client secrets
		GoogleClientSecrets clientSecrets = null;
		GoogleAuthorizationCodeFlow flow = null;
		try {
			clientSecrets = GoogleClientSecrets.load(
					JSON_FACTORY,
					new InputStreamReader(YouTubeGrabber.class
							.getResourceAsStream("/client_secrets.json")));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (clientSecrets.getDetails().getClientId().startsWith("Enter")
				|| clientSecrets.getDetails().getClientSecret()
						.startsWith("Enter ")) {
			System.out.println("Overwrite the src/main/resources/client_secrets.json file with the client secrets file "
							+ "you downloaded from the Quickstart tool or manually enter your Client ID and Secret "
							+ "from https://code.google.com/apis/console/?api=youtube#project:498789533454 "
							+ "into src/main/resources/client_secrets.json");
			System.exit(1);
		}
		Set<String> scopes = new HashSet<String>();
		scopes.add(YouTubeScopes.YOUTUBE);
		scopes.add(YouTubeScopes.YOUTUBE_READONLY);
		scopes.add(YouTubeScopes.YOUTUBE_UPLOAD);
		scopes.add(YouTubeScopes.YOUTUBEPARTNER);
		scopes.add(YouTubeScopes.YOUTUBEPARTNER_CHANNEL_AUDIT);

		try {
			flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, 
												JSON_FACTORY, 
												clientSecrets, 
												scopes)
											.setDataStoreFactory(dataStoreFactory).build();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			credential  = new AuthorizationCodeInstalledApp(flow,new LocalServerReceiver()).authorize("user");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		credential.setExpiresInSeconds(1000L);
		return true;
	}

	private void checkAndExtendToken() {
		Long validity = credential.getExpiresInSeconds();
		if ( validity == null && validity <= 0 ) { 
			authorize();
		} else if ( validity != null && validity < 5 ) {
			credential.setExpiresInSeconds(1000L);
		} 
	}
	private static Date convertStringtoDate(String dateTime) {
		String dateString = dateTime.split("T")[0];				
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		Date _date = null;
		try {	 
			_date = (java.sql.Date) formatter.parse(dateString);
			//System.out.println(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}		
		return _date;
		
	}
	private void updateVideos(String playListId ) {
		PGSQlConnectionClass sqlConnection =   new PGSQlConnectionClass(_host, _port, _dbname, _username, _password);
		PlaylistItemListResponse playListResult = null;
		String nextPageToken =  null ;
		String publishedAt = null;
		Integer ytID = null;
		PreparedStatement pStmt = null; 
		checkAndExtendToken();
		/*try {
			System.out.println(" Channel request in JSON " 	+ playListResult.toPrettyString());
		} catch (IOException e) {			
			e.printStackTrace();
		}*/
		java.util.List<PlaylistItem> playList = null;

		//nextPageToken = playListResult.getNextPageToken();

		do {
			try {
				playListResult = youtube.playlistItems()
									.list("snippet")
									.setFields("snippet/title")
									.setOnBehalfOfContentOwner(ON_BEHALF_OF_OWNER)
									.setMaxResults(500L)
									.setPlaylistId(playListId)
									.setPageToken(nextPageToken)
									//.setPlaylistId("22")
									.execute();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();//The Snippet title "+ publishedAt );
			}

			playList = playListResult.getItems();
			nextPageToken = playListResult.getNextPageToken();
			if ( playList == null) { 
				System.out.println(" None returned from a call to playlist items ");
				return;
			}
			// check if anything is returned. if not break out of this while loop.
			for (PlaylistItem _playItem : playList) {
				if ( _playItem.getSnippet() != null ) { 
					if ( _playItem.getSnippet().getTitle() != null) {
						publishedAt = _playItem.getSnippet().getTitle();
						System.out.println(" The Snippet title "+ publishedAt );
					}
	
					if (_playItem.getSnippet().getResourceId().getVideoId() != null	&& 
						_playItem.getSnippet().getResourceId().getVideoId() != null) {
						ytID = Integer.parseInt(_playItem.getSnippet().getResourceId().getVideoId());
						System.out.println(" The Snippet title "+ ytID );
					}
				}	

				try {
					pStmt = sqlConnection.getPGSQLConnection().prepareStatement(" Update ytvideo set publishedat = ? Where youtubeid = ? ");
					pStmt.setDate(1, (java.sql.Date) YouTubeGrabber.convertStringtoDate(publishedAt));
					pStmt.setInt(2, ytID);
					pStmt.executeUpdate();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} while ( (playListResult.getNextPageToken() != null )  && (playListResult.getNextPageToken() != "") );
		sqlConnection.closeConnection();
	}	
	
	
	private void processChannels() {	
		PGSQlConnectionClass sqlConnection =   new PGSQlConnectionClass(_host, _port, _dbname, _username, _password);
		YouTube.Channels.List channelRequest = null;
		ChannelListResponse channelResult = null;
		PreparedStatement prepst = null; 
		Integer ytID = null;
		String	ytTitle = null;

		try {
			channelRequest = youtube.channels().list("snippet,contentDetails");
			channelRequest.setMine(true);
			channelRequest
					.setFields("items/contentDetails, items/snippet/title ");
			channelResult = channelRequest.execute();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			System.out.println(" Channel request in JSON "
					+ channelResult.toPrettyString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		java.util.List<Channel> channelList = channelResult.getItems();

		for (Channel _channel : channelList) {
			if (_channel.getSnippet() != null
					&& _channel.getSnippet().getTitle() != null) {
				ytTitle	= _channel.getSnippet().getTitle();	
				System.out.println(" The Snippet title " + ytTitle );
			}
			if (_channel.getContentDetails() != null
					&& _channel.getContentDetails().getRelatedPlaylists() != null
					&& _channel.getContentDetails().getRelatedPlaylists()
							.getUploads() != null) {
				ytID	= Integer.parseInt(_channel.getContentDetails().getRelatedPlaylists().getUploads());
				System.out.println(" The  uploads " + ytID );
			}
			try {
				prepst = sqlConnection.getPGSQLConnection().prepareStatement("Insert into ytchannel  values ( 0, "+  ytID + ", '" + ytTitle  + "' " );
				prepst.executeUpdate();
				prepst.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
		sqlConnection.closeConnection();
	}

 	private void processVideos() { 
 		PGSQlConnectionClass sqlConnection =   new PGSQlConnectionClass(_host, _port, _dbname, _username, _password);
		Statement stmt = null;
		ResultSet rs = null;
		try {
			sqlConnection.getPGSQLConnection().setAutoCommit(false);
			stmt = sqlConnection.getPGSQLConnection().createStatement();
			stmt.setFetchSize(50);
			rs = stmt.executeQuery(" Select * from ytchannel ");
			while (rs.next())
			{
			    System.out.print("a row was returned.");
				//Call the getPlayListItems(youtube, rs.?????????)
			}
						
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally { 
			try {
				rs.close();
				stmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
		}
		sqlConnection.closeConnection();
	}

 	
	private void processYTAnalytics() { 
		PGSQlConnectionClass sqlConnection =   new PGSQlConnectionClass(_host, _port, _dbname, _username, _password);
		PGSQlConnectionClass sqlConnection2 =   new PGSQlConnectionClass(_host, _port, _dbname, _username, _password);
		
		String sqlStatement = 	" SELECT ytvideo.id,	ytvideo.publishedat,	ytvideo.youtubeid,	ytvideo.title, " +
								" min(ytanalytic.adate) as min_date "+ 
								" FROM ytvideo, ytanalytic " + 
								" WHERE ytvideo.id = ytanalytic.ytvideo_id " + 
								" AND  ytvideo.ready = FALSE " + 
								" GROUP BY ytvideo.id, ytvideo.publishedat, ytvideo.youtubeid, ytvideo.title " +
								" HAVING min(ytanalytic.adate) > ytvideo.publishedat " +
								" ORDER BY ytvideo.publishedat DESC ";

		Statement stmt = null;
		ResultSet rs = null;
		Date	requestStart	= null;
		Date	publishedAt		= null;
		Date	requestEnd	= null;
		String sqlString = null;
		Integer currentID = null;
		DMLClass dmlClass = new DMLClass(sqlConnection2.getPGSQLConnection());		
		
		try {
			dmlClass.setAutoCommit(false);
			dmlClass.setSQLStatement(" Update ytvideo set ready = ?  WHERE id = ? ");
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		
		try {
			sqlConnection.getPGSQLConnection().setAutoCommit(false);
			stmt = sqlConnection.getPGSQLConnection().createStatement();			
			rs = stmt.executeQuery(sqlStatement);
			
			while (rs.next())
			{
				publishedAt		=	rs.getDate(2);
				requestStart	=	rs.getDate(5);
				currentID		=	rs.getInt(1);
				
				System.out.print("a row was returned.");
				while ( requestStart.after(publishedAt) ) {
					requestEnd = UtilFunctions.addDays(requestStart, -1 );
					requestStart = UtilFunctions.addDays(requestStart, -31 );
					Boolean _ready = false;
					if ( requestStart.compareTo(publishedAt) <= 0 ) { 
						requestStart = UtilFunctions.addDays(publishedAt, -1 );
						_ready = true;
						dmlClass.setBoolean(1, true);
						dmlClass.setInteger(2, currentID);
						dmlClass.addStatementToBatch();
						if ( dmlClass.getCountBatch() > 100) {
							dmlClass.executeBatch();
						}
					}
					checkAndExtendToken();
					queryAnalytics(currentID, requestStart, requestEnd);				
				} 
			}
			if ( dmlClass.getCountBatch() > 0) {
				dmlClass.executeBatch();
			}
			dmlClass.commitStatements();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {			
			try {
				rs.close();
				stmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		sqlConnection.closeConnection();
		sqlConnection2.closeConnection();
	}

	private static String makeDBURL(String host, String port, String dbname) {
		String dburl = "jdbc:postgresql://" + host + ":" + port + "/" + dbname;
		//System.out.println(" The dburl " + dburl );
		return dburl;
	}

	/*private void getPGSQLConnection() {
		connection = null;
		try {
			Class.forName("org.postgresql.Driver");
			connection = DriverManager.getConnection(
					YouTubeGrabber.makeDBURL(_host, _port, _dbname), 
					_username,
					_password);
		} catch (ClassNotFoundException e) {
			System.out.println("ERROR: PostgreSQL JDBC Driver not found in the library path.");
		} catch (SQLException e) {
			System.out.println("Connection Failed! Check output console" + e.getMessage());
			e.printStackTrace();
		}
	}*/
	
	private void testSQLConnection() {
		PGSQlConnectionClass sqlConnection =   new PGSQlConnectionClass(_host, _port, _dbname, _username, _password);
		Statement st = null;		
		PreparedStatement prepst = null;
		try {
			prepst = sqlConnection.getPGSQLConnection().prepareStatement("Insert into ytchannel (id, title, youtubeid)  values ( 0, ?, ? ) ");
			prepst.setString(1, "ZeeTV");
			prepst.setString(2, "Zee Television");
			prepst.executeUpdate();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} finally { 
			try {
				prepst.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
		
		try {
			st = sqlConnection.getPGSQLConnection().createStatement();
			ResultSet rs = st.executeQuery("SELECT * FROM ytchannel WHERE id = 1 ");
			while (rs.next())
			{
			   System.out.println("Title: " + rs.getString("title") + 
					   			" You tube ID: " + rs.getString("youtubeid"));
			} rs.close();
			st.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sqlConnection.closeConnection();
	}
	
	//private void add
	/*
	private void closeConnection() {
		if ( connection == null) {
			System.out.println(" The connection is null");
			return;
		}
		try {
			connection.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/
	
	
	public void instantiateYouTube() { 
		try {
				httpTransport = GoogleNetHttpTransport.newTrustedTransport();
				dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
		} catch (GeneralSecurityException e) {
				// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		authorize();
	    youtube = new YouTube.Builder(httpTransport,JSON_FACTORY, credential)
	          					.setApplicationName(APPLICATION_NAME)
	          					.build();
	}
	
	public void instantiateYouTubeAnalytics() {
		
		try {
				httpTransport = GoogleNetHttpTransport.newTrustedTransport();
				dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
		} catch (GeneralSecurityException e) {
				// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		authorize();
		analytics = new YouTubeAnalytics.Builder(httpTransport, 
													JSON_FACTORY, 
													credential)
										.setApplicationName(APPLICATION_NAME)
										.build();		
	}	
	
	public void queryAnalytics(Integer ytVideoID, Date startDate, Date endDate) {
		PGSQlConnectionClass sqlConnection =   new PGSQlConnectionClass(_host, _port, _dbname, _username, _password);
		PGSQlConnectionClass sqlConnection2 =   new PGSQlConnectionClass(_host, _port, _dbname, _username, _password);
		PreparedStatement pStmt = null;
		try {
			pStmt = sqlConnection.getPGSQLConnection().prepareStatement("	SELECT count(*) from ytanalytic " 	+ 
																							"	WHERE ytvideo_id = ? "		+ 
																							"	AND adate = $2	");
		} catch (SQLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		DMLClass dmlClass = new DMLClass(sqlConnection2.getPGSQLConnection());		
		String sqlInsert = 	"	INSERT INTO  ytanalytic  (ytvideo_id,	adate,	territory, 	views,	comments, " +
							"	favoritesadded,	favoritesremoved,	likes,	dislikes,	shares,	" + 
							"	annotationClickthroughrate,	annotationcloserate,	subscribersgained,	" + 
							"	subscriberslost,	uniques) "	 +  
							"	VALUES ( ?, ?, ?, ?, ?, ?, "	+
							"	?, ?, ?, ?, ?, ?, "	+
							"	?, ?, ? ) " ;
		
		////"	VALUES ( {$row['id']},'{$data[0]}','$territory','{$data[1]}','{$data[2]}','{$data[3]}','{$data[4]}','{$data[5]}','{$data[6]}','{$data[7]}','{$data[8]}','{$data[9]}','{$data[10]}','{$data[11]}','{$data[12]}');\n";
		
		try {
			dmlClass.setAutoCommit(false);
			dmlClass.setSQLStatement(sqlInsert);
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		
		//try {		sqlConnection.getPGSQLConnection().setAutoCommit(false);
		//stmt = sqlConnection.getPGSQLConnection(
	
		ResultSet rs = null;			
	    ResultTable reportOut = null;
		try {
			reportOut = analytics.reports()	    
						            .query("contentOwner==WMG",
						            		UtilFunctions.dateToString(startDate),
						            		UtilFunctions.dateToString(endDate),
						            		" views,comments,favoritesAdded," 	+ 
						            		" favoritesRemoved,likes,dislikes,"	+
						            		" shares,annotationClickThroughRate," +
						            		" annotationCloseRate, subscribersGained,subscribersLost,uniques")
						            		.setFilters("video=="+ytVideoID+";dimensions==day")
						                   	.execute();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		for ( java.util.List<Object>  row : reportOut.getRows()) { 
			try {
				pStmt.setInt(1,ytVideoID);
				pStmt.setDate(2, (java.sql.Date) startDate);
				rs = pStmt.executeQuery();
				if ( rs.first()	) {
					System.out.println(" Data already stored for: " + startDate );  
				} else { 
					//SRAJAN
					//Insert....................
					////"	VALUES ( {$row['id']},'{$data[0]}','$territory','{$data[1]}',
					// '{$data[2]}','{$data[3]}','{$data[4]}','{$data[5]}','{$data[6]}','{$data[7]}','{$data[8]}','{$data[9]}','{$data[10]}','{$data[11]}','{$data[12]}');\n";
					dmlClass.setInteger(1, ytVideoID);
					dmlClass.setDate(2, startDate);
					dmlClass.setString(3, " ");
					dmlClass.setInteger(4, ((BigDecimal) row.get(0)).intValue() );//views
					dmlClass.setInteger(5, ((BigDecimal) row.get(1)).intValue() );//comments
					dmlClass.setInteger(6, ((BigDecimal) row.get(2)).intValue() );//favoritesadded
					dmlClass.setInteger(7, ((BigDecimal) row.get(3)).intValue() );//favoritesremoved
					dmlClass.setInteger(8, ((BigDecimal) row.get(4)).intValue() );//likes
					dmlClass.setInteger(9, ((BigDecimal) row.get(5)).intValue() );//dislikes
					dmlClass.setInteger(10, ((BigDecimal) row.get(6)).intValue() );//shares
					dmlClass.setInteger(11, ((BigDecimal) row.get(7)).intValue() );//annotationclickthroughrate
					dmlClass.setInteger(12, ((BigDecimal) row.get(8)).intValue() );//annotationcloserate
					dmlClass.setInteger(13, ((BigDecimal) row.get(9)).intValue() );//subscribersgained
					dmlClass.setInteger(14, ((BigDecimal) row.get(10)).intValue() );//subscriberslost
					dmlClass.setInteger(15, ((BigDecimal) row.get(11)).intValue() );//uniques
					dmlClass.addStatementToBatch();
					if ( dmlClass.getCountBatch() > 100) {
						dmlClass.executeBatch();
					}
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			dmlClass.executeBatch();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		sqlConnection.closeConnection();
		sqlConnection2.closeConnection();
	}
	
	
	public static void main(String[] args) {
		YouTubeGrabber ytGrabber = new YouTubeGrabber();
		//ytGrabber.getPGSQLConnection();
		//ytGrabber.testSQLConnection();	
		//ytGrabber.closeConnection();
		//System.out.println(" dbname as parameter " + ytGrabber._dbname );
		System.out.println(" After adding 15 days " + UtilFunctions.addDays(new Date(), 2));
		ytGrabber.instantiateYouTube();
		ytGrabber.processChannels();
		ytGrabber.updateVideos(null);      
		System.out.println("Success! Now add code here.");

	    System.exit(1);
	    
  }
}

//	http://jdbc.postgresql.org/documentation/head/ssl.html
//	