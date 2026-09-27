package com.example.dividend;
import javax.persistence.*;
@Entity public class Measurement {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long assetId;
 public String assetCode,assetName;
 @Column(nullable=false,length=100) public String title;
 public String createdAt,updatedAt;
 @Lob @Column(nullable=false) public String inputJson;
}
