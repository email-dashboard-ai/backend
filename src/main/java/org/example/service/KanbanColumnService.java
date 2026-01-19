package org.example.service;

import java.io.IOException;
import java.util.List;
import org.example.dto.request.KanbanColumnRequest;
import org.example.dto.response.KanbanColumnResponse;

/** Service for managing Kanban columns */
public interface KanbanColumnService {

  /**
   * Get all columns for the current user
   *
   * @param userEmail User's email
   * @return List of columns ordered by position
   */
  List<KanbanColumnResponse> getUserColumns(String userEmail);

  /**
   * Create a new Kanban column
   *
   * @param userEmail User's email
   * @param request Column creation request
   * @return Created column
   * @throws IOException if Gmail label creation fails
   */
  KanbanColumnResponse createColumn(String userEmail, KanbanColumnRequest request)
      throws IOException;

  /**
   * Update an existing column
   *
   * @param userEmail User's email
   * @param columnId Column database ID
   * @param request Update request
   * @return Updated column
   * @throws IOException if Gmail label update fails
   */
  KanbanColumnResponse updateColumn(String userEmail, Long columnId, KanbanColumnRequest request)
      throws IOException;

  /**
   * Delete a column
   *
   * @param userEmail User's email
   * @param columnId Column database ID
   * @throws IOException if Gmail label deletion fails
   */
  void deleteColumn(String userEmail, Long columnId) throws IOException;

  /**
   * Initialize default columns for a new user
   *
   * @param userEmail User's email
   */
  void initializeDefaultColumns(String userEmail);

  /**
   * Move an email to a specific column (updating status and Gmail labels)
   *
   * @param userEmail Email of the user
   * @param emailId ID of the email to move
   * @param columnId ID of the target column
   */
  void moveEmailToColumn(String userEmail, String emailId, String columnId);
}
