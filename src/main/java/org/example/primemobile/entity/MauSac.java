package org.example.primemobile.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mau_sac")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MauSac {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ten_mau", nullable = false, length = 100, unique = true)
    private String tenMau;

    @Column(name = "mo_ta", length = 255)
    private String moTa;

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    @OneToMany(mappedBy = "mauSac", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<BienTheSanPham> bienTheSanPhams = new ArrayList<>();
}
