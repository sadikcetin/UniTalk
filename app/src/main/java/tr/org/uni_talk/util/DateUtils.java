package tr.org.uni_talk.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {

    private static DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
    private static DateFormat timeFormat = new SimpleDateFormat("H:mm");

    public static String getCurrentTime() {

        Date today = Calendar.getInstance().getTime();
        return timeFormat.format(today);
    }

    public static String getCurrentDate() {

        Date today = Calendar.getInstance().getTime();
        return dateFormat.format(today);
    }
}
