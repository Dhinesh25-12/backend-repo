package com.insurance.portal.service;

import com.insurance.portal.dto.request.ProductRequest;
import com.insurance.portal.dto.response.ProductResponse;
import com.insurance.portal.entity.Product;
import com.insurance.portal.exception.ResourceNotFoundException;
import com.insurance.portal.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.name())
                .category(request.category())
                .description(request.description())
                .coverageAmount(request.coverageAmount())
                .premiumAmount(request.premiumAmount())
                .tenureMonths(request.tenureMonths())
                .active(true)
                .build();
        product = productRepository.save(product);
        return toResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findById(id);
        product.setName(request.name());
        product.setCategory(request.category());
        product.setDescription(request.description());
        product.setCoverageAmount(request.coverageAmount());
        product.setPremiumAmount(request.premiumAmount());
        product.setTenureMonths(request.tenureMonths());
        productRepository.save(product);
        return toResponse(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = findById(id);
        product.setActive(false);
        productRepository.save(product);
    }

    public ProductResponse getProduct(Long id) {
        return toResponse(findById(id));
    }

    public Page<ProductResponse> search(String category, String name, Pageable pageable) {
        Page<Product> page;
        if (StringUtils.hasText(category)) {
            page = productRepository.findByCategoryIgnoreCaseAndActiveTrue(category, pageable);
        } else if (StringUtils.hasText(name)) {
            page = productRepository.findByNameContainingIgnoreCaseAndActiveTrue(name, pageable);
        } else {
            page = productRepository.findByActiveTrue(pageable);
        }
        return page.map(this::toResponse);
    }

    public List<ProductResponse> compare(List<Long> ids) {
        return productRepository.findAllById(ids).stream().map(this::toResponse).toList();
    }

    private Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getCategory(),
                product.getDescription(), product.getCoverageAmount(), product.getPremiumAmount(),
                product.getTenureMonths(), product.isActive());
    }
}
