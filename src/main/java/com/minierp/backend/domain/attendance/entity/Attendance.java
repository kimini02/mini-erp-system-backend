package com.minierp.backend.domain.attendance.entity;

import com.minierp.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "attendances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "work_date"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Attendance extends BaseEntity {
}
