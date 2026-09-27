package com.example.dividend;
import javax.persistence.*;
import java.math.BigDecimal;
@Entity @Table(uniqueConstraints=@UniqueConstraint(columnNames="code"))
public class Asset {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=32) public String code;
 @Column(nullable=false,length=100) public String name;
 @Column(length=100) public String industry;
 @Column(precision=30,scale=8) public BigDecimal price,shares,marketCap;
}
