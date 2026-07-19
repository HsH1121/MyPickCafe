package com.example.MyPickCafe.service;

import com.example.MyPickCafe.dto.MenuResponse;
import com.example.MyPickCafe.dto.MenuUpdateRequest;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.Menu;
import com.example.MyPickCafe.repository.MenuRepository;
import com.example.MyPickCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository repository;

    @Transactional(readOnly = true)
    public List<Menu> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Menu findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Menu not found: " + id));
    }

    // ===== REST API용 (엔티티 대신 DTO 반환) =====
    // 매핑을 트랜잭션 안에서 끝내 open-in-view에 의존하지 않는다.

    @Transactional(readOnly = true)
    public MenuResponse findByIdForApi(Long id) {
        return MenuResponse.from(findById(id));
    }

    @Transactional(readOnly = true)
    public List<MenuResponse> findByCafeIdForApi(Long cafeId) {
        return repository.findByCafe_Id(cafeId).stream().map(MenuResponse::from).toList();
    }

    @Transactional
    public MenuResponse createMenu(Cafe cafe, String name, int price,
                                   boolean isNew, boolean isRecommended, String photoUrl) {
        Menu m = new Menu();
        m.setCafe(cafe);            // 소속 카페는 호출부에서 소유권 검증을 마친 값
        m.setName(name);
        m.setPrice(price);
        m.setNew(isNew);
        m.setRecommended(isRecommended);
        m.setPhoto(photoUrl);
        return MenuResponse.from(repository.save(m));
    }

    /** 더티 체킹으로 변경분만 반영. 소속 카페는 변경 대상이 아니다. */
    @Transactional
    public MenuResponse updateMenu(Long id, MenuUpdateRequest req) {
        Menu m = findById(id);
        if (req.name() != null && !req.name().isBlank()) m.setName(req.name());
        if (req.price() != null)           m.setPrice(req.price());
        if (req.isNew() != null)           m.setNew(req.isNew());
        if (req.isRecommended() != null)   m.setRecommended(req.isRecommended());
        return MenuResponse.from(m);
    }

    @Transactional
    public void updatePhoto(Long id, String photoUrl) {
        findById(id).setPhoto(photoUrl);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Menu not found: " + id);
        }
        repository.deleteById(id);
    }
    @Transactional(readOnly = true)
    public List<Menu> findByCafeId(Long cafeId) {
        return repository.findByCafe_Id(cafeId);
    }
}
