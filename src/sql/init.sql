drop table sys_user;
create table sys_user (
  user_id INT not null primary key auto_increment ,
  open_id VARCHAR(32) not null unique
);