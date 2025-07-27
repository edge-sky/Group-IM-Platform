package cn.lut.imserver.util.interceptor;

import cn.lut.imserver.service.ConversationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PermissionCheckInterceptor implements HandlerInterceptor {
    // TODO: 使用 redis 提高性能
    @Autowired
    private ConversationService conversationService;

    // 需要检查文件操作权限的路径模式
    private final List<String> fileVisiblePatterns = List.of(
            ".*/file/versionList",
            ".*/\\d+/folder/list"      // 匹配 /{数字}/folder/list
    );

    private final List<String> fileOperatePatterns = List.of(
            ".*/file/upload",
            ".*/file/upload/getUrl",
            ".*/file/upload/confirm",
            ".*/file/move",
            ".*/file/remove",
            ".*/\\d+/folder/create",   // 匹配 /{数字}/folder/create
            ".*/\\d+/folder/move",
            ".*/\\d+/folder/remove"
    );

    private final List<String> memberOperationPatterns = List.of(
            ".*/\\d+/conversation/invite",
            ".*/\\d+/conversation/removeUser"
    );

//    private final List<String> messageOperationPatterns = List.of(
//            "/message/send",
//            "/message/get"
//    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestPath = request.getRequestURI();

        // 判断是否需要权限检查
        boolean needFileVisiblePermissionCheck = matchesAnyPattern(requestPath, fileVisiblePatterns);
        boolean needFileOperatePermissionCheck = matchesAnyPattern(requestPath, fileOperatePatterns);
        boolean needMemberPermissionCheck = matchesAnyPattern(requestPath, memberOperationPatterns);

        if (!needFileVisiblePermissionCheck && !needMemberPermissionCheck && !needFileOperatePermissionCheck) {
            // 不需要权限检查，直接放行
            return true;
        }

        // 正则表达式匹配会话 id
        Pattern pattern = Pattern.compile("^/(1\\d*).*");
        Matcher matcher = pattern.matcher(requestPath);
        String conversationId;
        if (matcher.find()) {
            conversationId = matcher.group(1);
        } else {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.getWriter().write("Invalid conversation ID");
            return false;
        }

        Object uidAttribute = request.getAttribute("uid");
        long uid;
        if (uidAttribute instanceof Long) {
            uid = (Long) uidAttribute;
        } else if (uidAttribute instanceof String) {
            uid = Long.parseLong((String) uidAttribute);
        } else {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.getWriter().write("Invalid user ID");
            return false;
        }
        // 检查用户是否有操作权限
        boolean hasPermission = conversationService.isOperatePermission(
                uid,
                Long.parseLong(conversationId),
                needMemberPermissionCheck,
                false, // 消息权限在 websocket 中处理
                needFileVisiblePermissionCheck,
                needFileOperatePermissionCheck
        );
        if (!hasPermission) {
            // 没有权限，返回错误
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write("Permission denied");
            return false;
        }

        // 将会话ID存入请求属性中，供后续处理使用
        request.setAttribute("conversationId", Long.valueOf(conversationId));
        // 有权限，放行
        return true;
    }

    /**
     * 判断路径是否匹配任一模式
     */
    private boolean matchesAnyPattern(String path, List<String> patterns) {
        for (String pattern : patterns) {
            if (path.matches(pattern)) {
                return true;
            }
        }
        return false;
    }
}
