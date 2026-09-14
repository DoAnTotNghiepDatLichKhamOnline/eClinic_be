package iuh.fit.fe.be_websatlichkham.modules.notification.service;

import java.util.List;

import iuh.fit.fe.be_websatlichkham.modules.notification.entity.Notification;

public interface NotificationService {

    List<Notification> findByUser(Long userId);

    long countUnread(Long userId);

}
