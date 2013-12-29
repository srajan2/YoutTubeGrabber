package wmg.datagrabber.google.youtubegrabber.cmdline;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class UtilFunctions {

 	@SuppressWarnings("unused")
	public static Date addDays(Date date, int numbertoAdd) {
 		Calendar cal = new GregorianCalendar();
 		//cal.set(date.getYear(), cal.getMonth(), cal.getDay() ); 		
 		cal.setTime(date);
 		//System.out.println(" Day of month " + Calendar.DAY_OF_MONTH ); 
 		//System.out.println(" Day of month " + Calendar.D );
 		cal.add(Calendar.DATE, numbertoAdd);
 		return cal.getTime();
 	}
 	
 	@SuppressWarnings("unused")
	public static String dateToString (Date aDate) {
 		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
 		String returnDate = null;
 		
 		returnDate = df.format(aDate);
 		System.out.println("Report Date: " + returnDate );
 		return returnDate;
 	}
	
}
