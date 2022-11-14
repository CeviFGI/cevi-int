package tools.cevi.service;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import tools.cevi.domain.ContactMessage;
import tools.cevi.infra.jpa.ContactEntity;

@ApplicationScoped
public class ContactService {
    @Transactional
    public void insert(String message) {
        ContactEntity entity = new ContactEntity();
        entity.message = message;
        entity.persist();
    }

    public List<ContactMessage> listContactMessages() {
        return ContactEntity.listAll().stream().map(e -> this.map((ContactEntity) e)).toList();
    }

    private ContactMessage map(ContactEntity entity) {
        return new ContactMessage(entity.message);
    }
}
