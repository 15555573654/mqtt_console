package com.ruoyi.web.controller.mqtt;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.IMqttService;

/**
 * MQTT连接Controller
 * 
 * @author ruoyi
 * @date 2025-01-19
 */
@RestController
@RequestMapping("/mqtt/connection")
public class MqttConnectionController extends BaseController
{
    @Autowired
    private IMqttService mqttService;

    /**
     * 连接MQTT服务器
     */
    @PreAuthorize("@ss.hasPermi('mqtt:connection:connect')")
    @Log(title = "MQTT连接", businessType = BusinessType.OTHER)
    @PostMapping("/connect")
    public AjaxResult connect(@RequestBody Map<String, String> params)
    {
        String username = params.get("username");
        String password = params.get("password");

        if (username == null || username.length() < 5)
        {
            return error("用户名长度至少5位");
        }

        boolean success = mqttService.connect(username, password);

        if (success)
        {
            return success("MQTT连接成功");
        }
        else
        {
            return error("MQTT连接失败");
        }
    }

    /**
     * 断开MQTT连接
     */
    @PreAuthorize("@ss.hasPermi('mqtt:connection:disconnect')")
    @Log(title = "MQTT断开", businessType = BusinessType.OTHER)
    @PostMapping("/disconnect")
    public AjaxResult disconnect()
    {
        mqttService.disconnect();
        return success("MQTT连接已断开");
    }

    /**
     * 获取MQTT连接状态
     */
    @PreAuthorize("@ss.hasPermi('mqtt:connection:status')")
    @GetMapping("/status")
    public AjaxResult getStatus()
    {
        Map<String, Object> result = new HashMap<>();
        result.put("connected", mqttService.isConnected());
        result.put("username", mqttService.getCurrentUsername());
        return success(result);
    }
}
