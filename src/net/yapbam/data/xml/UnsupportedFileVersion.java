package net.yapbam.data.xml;

/** Signals that we tried to parse a stream that contains a yapbam file with a format not supported by this Yapbam version.
 * <br>This exception is thrown when pointing to a file created by a newer version of Yapbam.
 */
public class UnsupportedFileVersion extends UnsupportedFormatException {
	private static final long serialVersionUID = 1L;

	UnsupportedFileVersion(int version) {
		super ("File is of version "+version+". Expected "+Serializer.CURRENT_VERSION+" or less");
	}
}
