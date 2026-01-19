package org.example.repository;

import java.util.List;
import java.util.Optional;
import org.example.model.KanbanColumn;
import org.example.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KanbanColumnRepository extends JpaRepository<KanbanColumn, Long> {

    /**
     * Find all columns for a user, ordered by position
     *
     * @param user The user
     * @return List of columns ordered by position
     */
    List<KanbanColumn> findByUserOrderByPositionAsc(User user);

    /**
     * Find a specific column by user and column ID
     *
     * @param user     The user
     * @param columnId The column identifier
     * @return Optional containing the column if found
     */
    Optional<KanbanColumn> findByUserAndColumnId(User user, String columnId);

    /**
     * Find a column by user and ID
     *
     * @param user The user
     * @param id   The column database ID
     * @return Optional containing the column if found
     */
    Optional<KanbanColumn> findByUserAndId(User user, Long id);

    /**
     * Delete a column by user and ID
     *
     * @param user The user
     * @param id   The column database ID
     */
    void deleteByUserAndId(User user, Long id);

    /**
     * Check if a column exists for a user with the given column ID
     *
     * @param user     The user
     * @param columnId The column identifier
     * @return true if exists
     */
    boolean existsByUserAndColumnId(User user, String columnId);
}
