package com.bookwise.order.repository;

import com.bookwise.order.entity.OrderStatus;
import com.bookwise.order.entity.PurchaseOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @EntityGraph(attributePaths = {"items", "items.book", "payment", "user"})
    List<PurchaseOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"items", "items.book", "payment", "user"})
    Optional<PurchaseOrder> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"items", "items.book", "payment", "user"})
    Optional<PurchaseOrder> findWithItemsById(Long id);

    @Query("""
            select case when count(o) > 0 then true else false end
            from PurchaseOrder o
            join o.items i
            where o.user.id = :userId
              and o.status = :status
              and i.book.id = :bookId
            """)
    boolean existsOwnedBook(@Param("userId") Long userId, @Param("status") OrderStatus status, @Param("bookId") Long bookId);
}
