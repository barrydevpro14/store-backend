package org.store.common.service;

import org.springframework.web.multipart.MultipartFile;
import org.store.common.dto.ExcelStockParseResult;

public interface IExcelEntreeStockRowService {

    ExcelStockParseResult parseRows(MultipartFile multipartFile);
}
