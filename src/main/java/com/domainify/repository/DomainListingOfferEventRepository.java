package com.domainify.repository;

import com.domainify.entity.DomainListingOfferEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DomainListingOfferEventRepository extends JpaRepository<DomainListingOfferEvent, Long> {

    List<DomainListingOfferEvent> findByOfferIdOrderByCreatedAtAscIdAsc(Long offerId);
}
