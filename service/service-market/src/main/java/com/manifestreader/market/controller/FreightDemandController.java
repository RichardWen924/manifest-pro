package com.manifestreader.market.controller;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.common.result.R;
import com.manifestreader.market.service.FreightDemandService;
import com.manifestreader.model.dto.FreightDemandAcceptRequest;
import com.manifestreader.model.dto.FreightDemandCreateRequest;
import com.manifestreader.model.dto.FreightDemandPageQuery;
import com.manifestreader.model.dto.FreightQuoteCreateRequest;
import com.manifestreader.model.vo.FreightDemandDetailVO;
import com.manifestreader.model.vo.FreightDemandVO;
import com.manifestreader.model.vo.FreightOrderVO;
import com.manifestreader.model.vo.FreightQuoteVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "货运市场-需求")
@RestController
@RequestMapping("/market/demands")
public class FreightDemandController {

    private final FreightDemandService freightDemandService;

    public FreightDemandController(FreightDemandService freightDemandService) {
        this.freightDemandService = freightDemandService;
    }

    @PostMapping
    public R<FreightDemandVO> create(@Valid @RequestBody FreightDemandCreateRequest request) {
        return R.ok(freightDemandService.createDemand(request));
    }

    @GetMapping("/page")
    public R<PageResult<FreightDemandVO>> page(FreightDemandPageQuery query) {
        return R.ok(freightDemandService.page(query));
    }

    @GetMapping("/mine/page")
    public R<PageResult<FreightDemandVO>> pageMine(FreightDemandPageQuery query) {
        return R.ok(freightDemandService.pageMineDemands(query));
    }

    @GetMapping("/{id}")
    public R<FreightDemandDetailVO> detail(@PathVariable Long id) {
        return R.ok(freightDemandService.detail(id));
    }

    @PostMapping("/{id}/cancel")
    public R<FreightDemandVO> cancel(@PathVariable Long id) {
        return R.ok(freightDemandService.cancelDemand(id));
    }

    @PostMapping("/{id}/quotes")
    public R<FreightQuoteVO> submitQuote(@PathVariable Long id, @Valid @RequestBody FreightQuoteCreateRequest request) {
        return R.ok(freightDemandService.submitQuote(id, request));
    }

    @GetMapping("/{id}/quotes")
    public R<List<FreightQuoteVO>> listQuotes(@PathVariable Long id) {
        return R.ok(freightDemandService.listQuotes(id));
    }

    @PostMapping("/{demandId}/quotes/{quoteId}/withdraw")
    public R<FreightQuoteVO> withdrawQuote(@PathVariable Long demandId, @PathVariable Long quoteId) {
        return R.ok(freightDemandService.withdrawQuote(demandId, quoteId));
    }

    @PostMapping("/{id}/accept")
    public R<FreightOrderVO> acceptQuote(@PathVariable Long id, @Valid @RequestBody FreightDemandAcceptRequest request) {
        return R.ok(freightDemandService.acceptQuote(id, request));
    }
}
