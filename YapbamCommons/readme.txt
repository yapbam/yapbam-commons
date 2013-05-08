This project contains common source code between Desktop and Android version of Yapbam.

Android notes :
---------------
The Android Logger implementation has a bug that prevent level lower that INFO to be effectively logged.
A workaround is to use the following code at application startup:
	// Remove buggy handlers from the root logger
	Logger logger = Logger.getAnonymousLogger().getParent();
	Handler[] handlers = logger.getHandlers();
	for (Handler handler : handlers) {
		logger.removeHandler(handler);
	}
	// Create a new bug free handler
	FixedAndroidHandler handler = new FixedAndroidHandler();
	logger.addHandler(handler);
	// Set the right level limit
	handler.setLevel(Level.FINEST);
	logger.setLevel(Level.FINEST);
	
FixedAndroidHandler.java is available in the same folder as this file.
