package org.store.common.service;

import org.springframework.web.multipart.MultipartFile;
import org.store.common.dto.ExcelParseResult;

public interface IExcelProductRowService {

    ExcelParseResult parseRows(MultipartFile multipartFile);
}
