package library.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用户详情视图（含借阅统计）")
public class UserDetailVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "姓名")
    private String realName;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "角色：0超级管理员 1普通用户 2协管员 3游客")
    private Integer role;

    @Schema(description = "状态：1正常 0封禁")
    private Integer status;

    @Schema(description = "注册时间")
    private String createTime;

    @Schema(description = "累计借阅次数")
    private Long totalBorrows;

    @Schema(description = "当前借阅中数量")
    private Long currentBorrows;

    @Schema(description = "已逾期次数（含已归还的逾期记录）")
    private Long overdueCount;

    @Schema(description = "已归还数量")
    private Long returnedCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Integer getRole() { return role; }
    public void setRole(Integer role) { this.role = role; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public Long getTotalBorrows() { return totalBorrows; }
    public void setTotalBorrows(Long totalBorrows) { this.totalBorrows = totalBorrows; }
    public Long getCurrentBorrows() { return currentBorrows; }
    public void setCurrentBorrows(Long currentBorrows) { this.currentBorrows = currentBorrows; }
    public Long getOverdueCount() { return overdueCount; }
    public void setOverdueCount(Long overdueCount) { this.overdueCount = overdueCount; }
    public Long getReturnedCount() { return returnedCount; }
    public void setReturnedCount(Long returnedCount) { this.returnedCount = returnedCount; }
}