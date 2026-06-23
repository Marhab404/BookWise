package com.bookwise.order.repository;

import com.bookwise.order.entity.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("""
            select case when count(oi) > 0 then true else false end
            from OrderItem oi
            where oi.book.id = :bookId
            """)
    boolean existsByBookId(@Param("bookId") Long bookId);
}
