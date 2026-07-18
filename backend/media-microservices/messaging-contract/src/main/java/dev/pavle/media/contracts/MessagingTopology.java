package dev.pavle.media.contracts;

public final class MessagingTopology {
  public static final String COMMAND_EXCHANGE = "media.rendition.commands";
  public static final String COMMAND_QUEUE = "transcoder.rendition.commands";
  public static final String COMMAND_ROUTING_KEY = "rendition.command.create";

  public static final String EVENT_EXCHANGE = "media.rendition.events";
  public static final String EVENT_QUEUE = "media.rendition.events";
  public static final String RUNNING_ROUTING_KEY = "rendition.event.running";
  public static final String SUCCEEDED_ROUTING_KEY = "rendition.event.succeeded";
  public static final String FAILED_ROUTING_KEY = "rendition.event.failed";

  public static final String NOTIFICATION_EXCHANGE = "media.notifications";
  public static final String NOTIFICATION_QUEUE = "notification.terminal.events";
  public static final String NOTIFICATION_ROUTING_KEY = "notification.terminal";

  private MessagingTopology() {}
}
