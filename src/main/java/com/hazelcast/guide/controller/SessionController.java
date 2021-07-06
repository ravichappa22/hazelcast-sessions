package com.hazelcast.guide.controller;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.internal.eviction.ExpirationManager;
import com.hazelcast.map.IMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.hazelcast.HazelcastKeyValueAdapter;
import org.springframework.http.MediaType;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
public class SessionController {

    private static final DateFormat formatter = new SimpleDateFormat("HH:mm:ss");

    private static String APP_ID = "WC";

    @Autowired
    private  HazelcastKeyValueAdapter hazelcastKeyValueAdapter;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    /**
     * Creates a session for the request if there is no session of the request.
     *
     * @param principal Principal value of the session to be created
     * @return Message indicating the session creation or abortion result.
     *
     */
    @PutMapping("/create")
    public String createSession(@RequestParam("principal") String principal, HttpServletRequest request,
                                HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        String sessionId = null;
        String userKey = null;
        if (session == null) {
            session = request.getSession();
            sessionId = session.getId();


            userKey = principal.concat("_") //user id
                               .concat(sessionId).concat("_") //session id
                               .concat(String.valueOf(Instant.now())).concat("_") //transaction id
                               .concat(APP_ID); //app id
            //session.setAttribute("principal", principal);
            //session.setAttribute(principalIndexName, principal);
            IMap userKeyiMap = hazelcastInstance.getMap(userKey);
            userKeyiMap.set("principal", principal, 30, TimeUnit.MINUTES);
            //hazelcastKeyValueAdapter.put("principal", principal, userKey);
            userKeyiMap.set("sessionId", session.getId(), 30, TimeUnit.MINUTES);
            //attributes.put("principal", session.getAttribute(principalIndexName));
            userKeyiMap.set("created", Instant.now(), 30, TimeUnit.MINUTES);
            userKeyiMap.set("last accessed", Instant.now(), 30, TimeUnit.MINUTES);
            //hazelcastKeyValueAdapter.put("created", formatter.format(new Date(session.getCreationTime())), userKey);
            //hazelcastKeyValueAdapter.put("last accessed", formatter.format(new Date(session.getLastAccessedTime())), userKey);
        } else {
                sessionId = session.getId();

            userKey  = Arrays.asList(request.getCookies()).stream().
                  filter(c -> c.getName().equalsIgnoreCase("userKey")).findFirst().get().getValue();
            }
        Cookie userKeyCookie = new Cookie("userKey", userKey);
        userKeyCookie.setHttpOnly(true);
        userKeyCookie.setSecure(true);
        userKeyCookie.setMaxAge(30*60); //this need to be set to user session age
        userKeyCookie.setPath("/");
        response.addCookie(userKeyCookie);

        return  "Session created: " + sessionId;
    }

    /**
     * Returns the current session's details if the request has a session.
     *
     * @return Session details
     */
    @GetMapping(value = "/info", produces = MediaType.TEXT_HTML_VALUE)
    public String getSessionInfo(HttpServletRequest request, @RequestParam("principal") String principal) {
        HttpSession session = request.getSession(false);
        //String userKey = principal.concat("_").concat(APP_ID);
        Cookie userKeyCookie = Arrays.asList(request.getCookies()).stream().
                        filter(c -> c.getName().equalsIgnoreCase("userKey")).findFirst().orElse(null);
        if(userKeyCookie == null) {
            throw new RuntimeException("userkey not provided to track the session");
        }
        String userKey = userKeyCookie.getValue();
        Map<String, Object> attributes = new LinkedHashMap();
        attributes.put("sessionId", hazelcastKeyValueAdapter.get("sessionId",userKey));
        attributes.put("principal", hazelcastKeyValueAdapter.get("principal", userKey));
        attributes.put("created", hazelcastKeyValueAdapter.get("created", userKey));
        attributes.put("last accessed", hazelcastKeyValueAdapter.get("last accessed", userKey));
        return toHtmlTable(attributes);
    }

    @DeleteMapping(value = "/logout")
    public boolean logout(HttpServletRequest request, @RequestParam(value = "principal", required = true) String principal) {
    Cookie userKeyCookie = Arrays.asList(request.getCookies()).stream().
          filter(c -> c.getName().equalsIgnoreCase("userKey")).findFirst().orElse(null);
    if(userKeyCookie == null) {
        throw new RuntimeException("userkey not provided to track the session");
    }
    String userKey = userKeyCookie.getValue();
    hazelcastKeyValueAdapter.deleteAllOf(userKey);
    hazelcastInstance.getMap(userKey).destroy();
    HttpSession session = request.getSession(false);
    session.invalidate();
    return true;
  }

  @DeleteMapping(value = "/deletemap")
  public boolean deleteAllMaps(@RequestParam(value = "mapname", required = true) String mapname) {
      hazelcastInstance.getMap(mapname).destroy();
      return true;
  }

    private String toHtmlTable(Map<String, Object> attributes) {
        StringBuilder html = new StringBuilder("<html>");
        html.append("<table border=\"1\" cellpadding=\"5\" cellspacing=\"5\">");
        attributes.forEach((k, v) -> addHtmlTableRow(html, k, v));
        html.append("</table></html>");
        return html.toString();
    }

    private void addHtmlTableRow(StringBuilder content, String key, Object value) {
        content.append("<tr>")
                    .append("<th>").append(key).append("</th>")
                    .append("<td>").append(value).append("</td>")
                .append("</tr>");
    }

}
