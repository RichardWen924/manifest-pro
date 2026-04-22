package com.manifestreader.user.controller.bill;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.common.result.R;
import com.manifestreader.user.model.dto.BillCreateRequest;
import com.manifestreader.user.model.dto.BillPageQuery;
import com.manifestreader.user.model.dto.BillParseRequest;
import com.manifestreader.user.model.dto.BillUpdateRequest;
import com.manifestreader.user.model.dto.ExtractedBillSaveRequest;
import com.manifestreader.user.model.vo.BillDetailVO;
import com.manifestreader.user.model.vo.BillVO;
import com.manifestreader.user.service.BillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户端-提单")
@RestController
@RequestMapping("/user/bills")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @Operation(summary = "提单分页")
    @GetMapping("/page")
    public R<PageResult<BillVO>> page(BillPageQuery query) {
        return R.ok(billService.page(query));
    }

    @Operation(summary = "提单详情")
    @GetMapping("/{id}")
    public R<BillDetailVO> detail(@PathVariable Long id) {
        return R.ok(billService.detail(id));
    }

    @Operation(summary = "创建提单")
    @PostMapping
    public R<BillVO> create(@Valid @RequestBody BillCreateRequest request) {
        return R.ok(billService.create(request));
    }

    @Operation(summary = "将模板抽取结果保存为业务提单")
    @PostMapping("/from-extracted-fields")
    public R<BillVO> saveExtractedFields(@Valid @RequestBody ExtractedBillSaveRequest request) {
        return R.ok(billService.saveExtractedFields(request));
    }

    @Operation(summary = "更新提单")
    @PutMapping("/{id}")
    public R<BillVO> update(@PathVariable Long id, @Valid @RequestBody BillUpdateRequest request) {
        return R.ok(billService.update(id, request));
    }

    @Operation(summary = "删除提单")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        billService.delete(id);
        return R.ok();
    }

    @Operation(summary = "解析提单")
    @PostMapping("/parse")
    public R<BillDetailVO> parse(@Valid @RequestBody BillParseRequest request) {
        return R.ok(billService.parse(request));
    }
}
