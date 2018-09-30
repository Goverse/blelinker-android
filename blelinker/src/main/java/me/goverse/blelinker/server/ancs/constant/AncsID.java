package me.goverse.blelinker.server.ancs.constant;

import java.nio.charset.Charset;

/**
 * @author gaoyu
 */
public class AncsID {

	/**
	 * charset_name
	 */
	public static final String CHARSETNAME = "UTF-8";

	/**
	 * charset
	 */
	public static final Charset CHARSET = Charset.forName(CHARSETNAME);

	/**
	 * Category
	 */
	public static class Category{
		/**
		 * OTHER
		 */
		public static final byte OTHER=0;

		/**
		 * INCOMING_CALL
		 */
		public static final byte INCOMING_CALL=1;

		/**
		 * MISSED_CALL
		 */
		public static final byte MISSED_CALL=2;

		/**
		 * VOICE_MAIL
		 */
		public static final byte VOICE_MAIL=3;

		/**
		 * SOCIAL
		 */
		public static final byte SOCIAL=4;

		/**
		 * SCHEDULE
		 */
		public static final byte SCHEDULE=5;

		/**
		 * EMAIL
		 */
		public static final byte EMAIL=6;

		/**
		 * NEWS
		 */
		public static final byte NEWS=7;

		/**
		 * HEALTH_AND_FITNESS
		 */
		public static final byte HEALTH_AND_FITNESS=8;

		/**
		 * BUSINESS_AND_FINANCE
		 */
		public static final byte BUSINESS_AND_FINANCE=9;

		/**
		 * LOCATION
		 */
		public static final byte LOCATION=10;

		/**
		 * ENTERTAINMENT
		 */
		public static final byte ENTERTAINMENT=11;

		/**
		 * RESERVED
		 */
		public static final byte RESERVED=12;
	}

	/**
	 * Event
	 */
	public static class Event{
		/**
		 * NOTIFICATION_ADDED
		 */
		public static final byte NOTIFICATION_ADDED=0;

		/**
		 * NOTIFICATION_MODIFIED
		 */
		public static final byte NOTIFICATION_MODIFIED=1;

		/**
		 * NOTIFICATION_REMOVED
		 */
		public static final byte NOTIFICATION_REMOVED=2;

		/**
		 * RESERVED
		 */
		public static final byte RESERVED=3;
	}

	/**
	 * AncsEventFlag
	 */
	public class AncsEventFlag {
		/**
		 * SILENT
		 */
		public static final byte SILENT = 1 << 0;

		/**
		 * IMPORTANT
		 */
		public static final byte IMPORTANT = 1 << 1;

		/**
		 * RESERVED
		 */
		public static final byte RESERVED = 1 << 2;
	}

	/**
	 * NotificationAttribute
	 */
	public static class NotificationAttribute{

		/**
		 * APP_IDENTIFIER
		 */
		public final static byte APP_IDENTIFIER = 0;

		/**
		 * TITLE
		 */
		public final static byte TITLE = 1;

		/**
		 * SUBTITLE
		 */
		public static final byte SUBTITLE = 2;

		/**
		 * MESSAGE
		 */
		public static final byte MESSAGE = 3;

		/**
		 * MESSAGE_SIZE
		 */
		public static final byte MESSAGE_SIZE = 4;

		/**
		 *DATE
		 */
		public static final byte DATE = 5;

		/**
		 * RESERVED
		 */
		public static final byte RESERVED = 6;
	}
}
