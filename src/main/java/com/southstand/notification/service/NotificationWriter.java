package com.southstand.notification.service;

import com.southstand.notification.entity.Notification;
import com.southstand.notification.mapper.NotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationWriter {
    private final NotificationMapper mapper;
    public NotificationWriter(NotificationMapper mapper){this.mapper=mapper;}

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void persist(Notification notification){mapper.insert(notification);}
}
