package com.bookwise.order.repository;

import com.bookwise.order.entity.OrderStatus;
import com.bookwise.order.entity.PurchaseOrder;
import java.time.Instant;
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

    long countByStatus(OrderStatus status);

    @Query("""
            select o from PurchaseOrder o
            where o.status = :status and o.createdAt >= :since
            """)
    List<PurchaseOrder> findByStatusAndCreatedAtAfter(@Param("status") OrderStatus status, @Param("since") Instant since);

    @Query("""
            select i.book.id, i.book.title, count(i) as cnt
            from OrderItem i
            join i.order o
            where o.status = :status
            group by i.book.id, i.book.title
            order by cnt desc
            """)
    List<Object[]> findTopSellingBooks(@Param("status") OrderStatus status);

    boolean existsByUserId(Long userId);
}
