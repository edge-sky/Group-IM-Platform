//package cn.lut.imserver.controller;
//
//import cn.lut.imserver.entity.Message;
//import cn.lut.imserver.entity.vo.MessageVo;
//import cn.lut.imserver.service.MessageService;
//import cn.lut.imserver.util.MqUtil;
//import cn.lut.imserver.util.RedisUtil;
//import com.alibaba.fastjson2.JSON;
//import com.alibaba.fastjson2.JSONObject;
//import jakarta.servlet.http.HttpServletRequest;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.http.ResponseEntity;
//import org.springframework.http.HttpStatus;
//
//import java.util.Date;
//import java.util.List;
//
//@RestController
//@RequestMapping("/{conversationId}/message")
//public class MessageController {
//    @Autowired
//    private MessageService messageService;
//    @Autowired
//    private RedisUtil redisUtil;
//    @Autowired
//    private MqUtil mqUtil;
//
//    @PostMapping("/send")
//    public ResponseEntity<JSONObject> sendMessage(@PathVariable("conversationId") long conversationId,
//                                                  @RequestParam("content") String content,
//                                                  @RequestParam("type") int type,
//                                                  HttpServletRequest request) {
//        long requestUid = (long) request.getAttribute("uid");
//        JSONObject jsonObject = new JSONObject();
//
//        // 封装 message
//        Message msg = new Message();
//        msg.setContent(content);
//        msg.setType(type);
//        msg.setConversationId(conversationId);
//        msg.setFromUid(requestUid);
//        msg.setTime(new Date());
//        msg.setWithdrawn(0);
//        msg.setMessageId(redisUtil.getIncrMessageId(conversationId));
//
//        try {
//            // 将消息加入消息队列
//            String messageJson = JSON.toJSONString(msg);
//            mqUtil.sendMessage("save-message", messageJson);
//        } catch (Exception e) {
//            jsonObject.put("code", "500");
//            jsonObject.put("message", "消息发送失败");
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonObject);
//        }
//
//        jsonObject.put("code", "200");
//        jsonObject.put("message", "消息发送成功");
//        return ResponseEntity.ok().body(jsonObject);
//    }
//
//    @GetMapping("/get")
//    public ResponseEntity<List<MessageVo>> getMessage(@PathVariable("conversationId") long conversationId,
//                                                      @RequestParam("earliestMessageId") long earliestMessageId,
//                                                      @RequestParam(value = "limit", defaultValue = "50") int limit,
//                                                      HttpServletRequest request) {
//        long requestUid = (long) request.getAttribute("uid");
//
//        List<MessageVo> messages = messageService.getMessages(conversationId, earliestMessageId, limit);
//        return ResponseEntity.ok(messages);
//    }
//}
