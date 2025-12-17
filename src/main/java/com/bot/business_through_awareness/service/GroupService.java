package com.bot.business_through_awareness.service;

import com.bot.business_through_awareness.model.Group;
import com.bot.business_through_awareness.repository.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GroupService {
    
    private final GroupRepository groupRepository;
    
    @Autowired
    public GroupService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }
    
    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }
    
    public Optional<Group> getGroupById(Long id) {
        return groupRepository.findById(id);
    }
    
    public Optional<Group> getGroupByName(String name) {
        return groupRepository.findByName(name);
    }
    
    public Group createGroup(String name) {
        if (groupRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Группа с таким именем уже существует");
        }
        Group group = new Group(name);
        return groupRepository.save(group);
    }
    
    public void deleteGroup(Long id) {
        groupRepository.deleteById(id);
    }
    
    public void deleteGroupByName(String name) {
        groupRepository.findByName(name).ifPresent(groupRepository::delete);
    }
}


