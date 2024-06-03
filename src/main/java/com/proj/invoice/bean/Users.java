package com.proj.invoice.bean;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

public class Users {
  @TableId(type = IdType.AUTO)
  private long account;
  private String pwd;
  private String position;


  public String getPosition() {
    return position;
  }

  public void setPosition(String position) {
    this.position = position;
  }

  public long getAccount() {
    return account;
  }

  public void setAccount(long account) {
    this.account = account;
  }

  public String getPwd() {
    return pwd;
  }

  public void setPwd(String pwd) {
    this.pwd = pwd;
  }


}
