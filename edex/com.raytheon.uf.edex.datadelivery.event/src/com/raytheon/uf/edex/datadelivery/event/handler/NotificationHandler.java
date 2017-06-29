package com.raytheon.uf.edex.datadelivery.event.handler;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.raytheon.uf.common.datadelivery.event.INotifiableEvent;
import com.raytheon.uf.common.datadelivery.event.notification.NotificationRecord;
import com.raytheon.uf.common.registry.event.RemoveRegistryEvent;

/**
 *
 * Handles the creating and storing of Notification records in the database and
 * sends a JMS notification message to the ddnotifyRoute end point. Also, this
 * handler processes requests from another JVM to retrieve Notification records.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Mar 01, 2012           sanchez   Initial creation
 * Jun 21, 2012  736      johnson   Change OPERATION_STATUS to OperationStatus.
 * Jul 05, 2012  740      johnson   Fix fall-through bug on switch statement.
 * Jul 06, 2012  740      johnson   Fix bug that assumes {@link
 *                                  RemoveRegistryEvent}s will only be received
 *                                  by the method with it as a parameter (i.e.
 *                                  superclass parameter methods receive it
 *                                  too).
 * Dec 07, 2012  1104     johnson   Changed to use INotifiableEvent for events
 *                                  with notifications.
 * Feb 05, 2013  1580     pduff     EventBus refactor.
 * Mar 18, 2013  1802     bphillip  Modified to use transactional boundaries and
 *                                  spring injection of daos
 * Mar 27, 2013  1802     bphillip  Moved event bus registration from
 *                                  PostConstruct method to Spring static method
 *                                  call
 * Jun 29, 2017  6130     tjensen   Add public storeAndSend override to allow
 *                                  for direct calling
 *
 * </pre>
 *
 * @author jsanchez
 * @version 1.0
 */
public class NotificationHandler extends AbstractHandler {

    private String endpoint;

    /**
     * Creates a new handler object and registers this object with the
     * DataDeliveryEventBus
     */
    public NotificationHandler() {
        super();
    }

    /**
     * Creates a new handler object and registers this object with the
     * DataDeliveryEventBus
     */
    public NotificationHandler(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Collects events for which a notification should be sent for.
     *
     * @param event
     *            the event generating the notification
     */
    @Subscribe
    @AllowConcurrentEvents
    public void eventListener(INotifiableEvent event) {
        NotificationRecord record = event.generateNotification();
        storeAndSend(record);
    }

    /**
     * Stores the record in the notification table.
     *
     * @param record
     */
    public void storeAndSend(NotificationRecord record) {
        storeAndSend(record, getEndpoint());
    }

    /**
     *
     * @return The end point the message will be sent to
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Set the value of the endpoint attribute. This value must be the URI of
     * the destination to use.
     *
     * @param endpoint
     *            The value to set the endpoint attribute to.
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

}
