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

    private final MenuRepository menuRepository;

    @Transactional(readOnly = true)
    public List<Menu> findAll() {
        return menuRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Menu findById(Long menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new NotFoundException("메뉴를 찾을 수 없습니다: " + menuId));
    }

    // ===== REST API용 (엔티티 대신 DTO 반환) =====
    // 매핑을 트랜잭션 안에서 끝내 open-in-view에 의존하지 않는다.

    @Transactional(readOnly = true)
    public MenuResponse findByIdForApi(Long menuId) {
        return MenuResponse.from(findById(menuId));
    }

    @Transactional(readOnly = true)
    public List<MenuResponse> findByCafeIdForApi(Long cafeId) {
        return menuRepository.findByCafe_Id(cafeId).stream().map(MenuResponse::from).toList();
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
        return MenuResponse.from(menuRepository.save(m));
    }

    /** 더티 체킹으로 변경분만 반영. 소속 카페는 변경 대상이 아니다. */
    @Transactional
    public MenuResponse updateMenu(Long menuId, MenuUpdateRequest req) {
        Menu m = findById(menuId);
        if (req.name() != null && !req.name().isBlank()) m.setName(req.name());
        if (req.price() != null)           m.setPrice(req.price());
        if (req.isNew() != null)           m.setNew(req.isNew());
        if (req.isRecommended() != null)   m.setRecommended(req.isRecommended());
        return MenuResponse.from(m);
    }

    @Transactional
    public void updatePhoto(Long menuId, String photoUrl) {
        findById(menuId).setPhoto(photoUrl);
    }

    @Transactional
    public void delete(Long menuId) {
        if (!menuRepository.existsById(menuId)) {
            throw new NotFoundException("메뉴를 찾을 수 없습니다: " + menuId);
        }
        menuRepository.deleteById(menuId);
    }
    @Transactional(readOnly = true)
    public List<Menu> findByCafeId(Long cafeId) {
        return menuRepository.findByCafe_Id(cafeId);
    }
}
