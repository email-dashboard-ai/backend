package org.example.service;

/**
 * Service for managing scheduled snooze operations. Automatically wakes up snoozed emails when
 * their snooze period expires.
 */
public interface SnoozeSchedulerService {

  /**
   * Wakes up emails that have reached their snooze deadline. This method is scheduled to run every
   * 30 seconds to check for snoozed emails that need to be moved back to the inbox.
   */
  void wakeUpSnoozeEmail();
}
