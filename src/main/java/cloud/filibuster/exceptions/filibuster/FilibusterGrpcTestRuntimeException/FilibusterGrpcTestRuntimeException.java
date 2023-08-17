package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import cloud.filibuster.exceptions.filibuster.FilibusterRuntimeException;

import java.util.List;

/**
 * {@code FilibusterGrpcTestRuntimeException} is the abstract superclass of the
 * exceptions that can be thrown from the GRPC test interface.
 *
 * <p>{@code FilibusterGrpcTestRuntimeException} and its subclasses are <em>unchecked
 * exceptions</em>.
 */
public abstract class FilibusterGrpcTestRuntimeException extends FilibusterRuntimeException {

    /**
     * Constructs a new Filibuster GRPC runtime exception with the specified detail message.
     *
     * @param message the detail message.
     */
    protected FilibusterGrpcTestRuntimeException(String message) {
        super(message);
    }

    /**
     * Constructs a new Filibuster GRPC runtime exception with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause.  (A {@code null} value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     */
    protected FilibusterGrpcTestRuntimeException(String message, Throwable cause) {
        super(message + "\n" + cause, cause);
    }

    /**
     * Returns the fix message for the exception.
     */
    abstract public String getFixMessage();

    public String generateSingleFixMessage(
            String descriptionText,
            String linkUrl
    ) {
        String linkText = linkUrl.substring(linkUrl.indexOf("statem/") + "statem/".length());
        return "<li>" + descriptionText + "<br /><a target=\"_blank\" href=\"" + linkUrl + "\"><span class=\"small_class_name\">" + linkText + "</span></a></li>";
    }

    public String generateFixMessage(List<String> fixMessages) {
        StringBuilder completedFixMessage = new StringBuilder();

        completedFixMessage.append("<ul>");
        for (String fixMessage : fixMessages) {
            completedFixMessage.append(fixMessage);
        }
        completedFixMessage.append("</ul>");

        return completedFixMessage.toString();
    }
}
