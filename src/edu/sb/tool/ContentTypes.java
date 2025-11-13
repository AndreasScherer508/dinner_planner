package edu.sb.tool;


/**
 * Facade for content type (MIME type) related operations.
 */
@Copyright(year=2023, holders="Sascha Baumeister")
public class ContentTypes {

	/**
	 * Prevents external instantiation.
	 */
	private ContentTypes () {}


	/**
	 * Returns whether or not the given content types are compatible to each other.
	 * @param leftContentType the left content type
	 * @param rightContentType the right content type
	 * @return {@code true} if both content types are compatible, {@code false} otherwise
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if any of the given arguments is not a valid content type
	 * 			because it contains more or less that one slash character
	 */
	static public boolean isCompatible (String leftContentType, String rightContentType) throws NullPointerException, IllegalArgumentException {
		leftContentType = leftContentType.split(";")[0];
		rightContentType = rightContentType.split(";")[0];

		final String[] leftContentParts = leftContentType.split("/");
		final String[] rightContentParts = rightContentType.split("/");
		if (leftContentParts.length != 2 | rightContentParts.length != 2) throw new IllegalArgumentException();

		for (int index = 0; index < leftContentParts.length; ++index) {
			if ("*".equals(leftContentParts[index]) | "*".equals(rightContentParts[index])) continue;
			if (!leftContentParts[index].equals(rightContentParts[index])) return false;
		}

		return true;
	}
}
