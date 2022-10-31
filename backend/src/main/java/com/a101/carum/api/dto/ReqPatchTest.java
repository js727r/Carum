package com.a101.carum.api.dto;

import com.a101.carum.domain.test.TestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReqPatchTest {
    String string;
    TestType type;
}