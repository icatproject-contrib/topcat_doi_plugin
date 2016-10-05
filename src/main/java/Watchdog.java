package org.icatproject.topcatdoiplugin;

import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.icatproject.ids.client.IdsClient;
import org.icatproject.ids.client.NotFoundException;

import org.icatproject.topcatdoiplugin.DoiDownload;
import org.icatproject.topcatdoiplugin.Properties;
import org.icatproject.topcatdoiplugin.Mailer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.validator.routines.EmailValidator;

@Singleton
public class Watchdog {

  private static final Logger logger = LoggerFactory.getLogger(Watchdog.class);
  private Map<Long, Date> lastChecks = new HashMap<Long, Date>();
  private AtomicBoolean busy = new AtomicBoolean(false);

  @PersistenceContext(unitName="topcat")
  EntityManager em;

  @EJB
  Mailer mailer;

  @Schedule(hour="*", minute="*")
  private void poll() {
    if(!busy.compareAndSet(false, true)){
      return;
    }

    Properties properties = Properties.getInstance();
    int pollDelay = Integer.parseInt(properties.getProperty("pollDelay"));
    int pollIntervalWait = Integer.parseInt(properties.getProperty("pollIntervalWait"));

    TypedQuery<DoiDownload> query = em.createQuery("select doiDownload from DoiDownload doiDownload where doiDownload.isEmailSent = false", DoiDownload.class);
    List<DoiDownload> doiDownloads = query.getResultList();

    for(DoiDownload doiDownload : doiDownloads){
      Date lastCheck = lastChecks.get(doiDownload.getId());
      Date now = new Date();
      long createdSecondsAgo = (now.getTime() - doiDownload.getCreatedAt().getTime()) / 1000;

      if(createdSecondsAgo >= pollDelay){
        if(lastCheck == null){
          performCheck(doiDownload);
        } else {
          long lastCheckSecondsAgo = (now.getTime() - lastCheck.getTime()) / 1000;
          if(lastCheckSecondsAgo >= pollIntervalWait){
            performCheck(doiDownload);
          }
        }
      }
    }

    busy.set(false);
  }

  private void performCheck(DoiDownload doiDownload) {
    try {
      IdsClient ids = new IdsClient(new URL(doiDownload.getTransportUrl()));
      if(ids.isPrepared(doiDownload.getPreparedId())){
        doiDownload.setIsEmailSent(true);
        em.persist(doiDownload);
        em.flush();
        lastChecks.remove(doiDownload.getId());
        sendDoiDownloadReadyEmail(doiDownload);
      } else {
        lastChecks.put(doiDownload.getId(), new Date());
      }
    } catch(NotFoundException e) {
      lastChecks.remove(doiDownload.getId());
    } catch(Exception e){
      logger.debug(e.toString());
    }
  }

  private void sendDoiDownloadReadyEmail(DoiDownload doiDownload){
    EmailValidator emailValidator = EmailValidator.getInstance();
    Properties properties = Properties.getInstance();

    if (doiDownload.getEmail() != null) {
      if (emailValidator.isValid(doiDownload.getEmail())) {

        String doiDownloadUrl = doiDownload.getTransportUrl();
        doiDownloadUrl += "/ids/getData?preparedId=" + doiDownload.getPreparedId();
        doiDownloadUrl += "&outname=" + doiDownload.getFileName();

        Map<String, String> valuesMap = new HashMap<String, String>();
        valuesMap.put("email", doiDownload.getEmail());
        valuesMap.put("preparedId", doiDownload.getPreparedId());
        valuesMap.put("doiDownloadUrl", doiDownloadUrl);
        valuesMap.put("fileName", doiDownload.getFileName());

        StrSubstitutor sub = new StrSubstitutor(valuesMap);
        String subject = sub.replace(properties.getProperty("doiDownloadReadyEmailSubject"));
        String message = sub.replace(properties.getProperty("doiDownloadReadyEmailMessage"));

        mailer.send(doiDownload.getEmail(), subject, message);

      } else {
        logger.debug("Email not sent. Invalid email " + doiDownload.getEmail());
      }
    }

  }

}