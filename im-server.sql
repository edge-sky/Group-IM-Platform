/*
 Navicat Premium Data Transfer

 Source Server         : 阿里云-广州
 Source Server Type    : MySQL
 Source Server Version : 80405
 Source Host           : 8.134.98.150:3306
 Source Schema         : im-server

 Target Server Type    : MySQL
 Target Server Version : 80405
 File Encoding         : 65001

 Date: 27/07/2025 20:13:14
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for conversation
-- ----------------------------
DROP TABLE IF EXISTS `conversation`;
CREATE TABLE `conversation`  (
  `id` bigint NOT NULL,
  `name` varchar(25) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `last_message_id` bigint NOT NULL,
  `user_num` int NULL DEFAULT NULL,
  `manager_uid` bigint NOT NULL,
  `deleted` int NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for conversation_user
-- ----------------------------
DROP TABLE IF EXISTS `conversation_user`;
CREATE TABLE `conversation_user`  (
  `conversation_id` bigint NOT NULL,
  `uid` bigint NOT NULL,
  `id` bigint NOT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  `member_permission` tinyint NOT NULL DEFAULT 0,
  `file_operate_permission` tinyint NOT NULL DEFAULT 0,
  `file_visible_permission` tinyint NOT NULL DEFAULT 0,
  `message_permission` tinyint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uid`(`uid` ASC, `conversation_id` ASC) USING BTREE,
  CONSTRAINT `chk_file_permission` CHECK ((`file_operate_permission` = 0) or (`file_visible_permission` = 1))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for file
-- ----------------------------
DROP TABLE IF EXISTS `file`;
CREATE TABLE `file`  (
  `id` bigint NOT NULL,
  `folder_id` bigint NOT NULL,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `latest_version` int NOT NULL,
  `update_time` datetime NOT NULL,
  `create_time` datetime NOT NULL,
  `deleted` tinyint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `folder_id`(`folder_id` ASC, `name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for file_version
-- ----------------------------
DROP TABLE IF EXISTS `file_version`;
CREATE TABLE `file_version`  (
  `id` bigint NOT NULL,
  `file_id` bigint NOT NULL,
  `comment` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `file_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `version` int NOT NULL,
  `update_uid` bigint NOT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for folder
-- ----------------------------
DROP TABLE IF EXISTS `folder`;
CREATE TABLE `folder`  (
  `id` bigint NOT NULL,
  `conversation_id` bigint NOT NULL,
  `pre_folder_id` bigint NOT NULL,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `update_uid` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `folder_pk`(`conversation_id` ASC, `pre_folder_id` ASC, `name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for message
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message`  (
  `id` bigint NOT NULL,
  `content` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `type` tinyint NOT NULL,
  `conversation_id` bigint NOT NULL,
  `message_id` bigint NOT NULL,
  `time` datetime NOT NULL,
  `withdrawn` tinyint NOT NULL,
  `from_uid` bigint NULL DEFAULT NULL,
  `is_read` tinyint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `uid` bigint NOT NULL,
  `username` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `salt` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` tinyint NOT NULL,
  `deleted` tinyint NOT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`uid`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
