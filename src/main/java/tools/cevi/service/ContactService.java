package tools.cevi.service;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import tools.cevi.domain.ContactMessage;
import tools.cevi.infra.jpa.ContactFormEntryEntity;

@ApplicationScoped
public class ContactService {
    @Transactional
    public void insert(String message) {
        ContactFormEntryEntity entity = new ContactFormEntryEntity();
        entity.message = message;
        entity.persist();
    }

    public List<ContactMessage> listContactMessages() {
        return ContactFormEntryEntity.listAll().stream().map(e -> this.map((ContactFormEntryEntity) e)).toList();
    }

    private ContactMessage map(ContactFormEntryEntity entity) {
        return new ContactMessage(entity.message);
    }
}
