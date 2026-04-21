package com.manifest.auth.converter;

import com.manifest.auth.vo.UserVO;
import com.manifestreader.model.entity.UserEntity;
import java.util.Collections;
import org.springframework.stereotype.Component;

@Component
public class AuthUserConverter {

    public UserVO toVO(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UserVO(
                entity.getId(),
                entity.getCompanyId(),
                entity.getUsername(),
                entity.getNickname(),
                entity.getMobile(),
                entity.getEmail(),
                entity.getStatus(),
                Collections.emptyList(),
                entity.getCreatedAt()
        );
    }
}
