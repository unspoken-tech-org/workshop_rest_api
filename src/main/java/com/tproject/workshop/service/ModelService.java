package com.tproject.workshop.service;

import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Model;
import com.tproject.workshop.repository.ModelRepository;
import com.tproject.workshop.utils.UtilsString;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ModelService {

    private final ModelRepository modelRepository;

    public List<Model> findAll(){
        return modelRepository.findAll();
    }

    public Model findById(int id){
        return modelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Modelo com id %d não encontrada", id)));
    }

    public Model save(Model model){
        var modelFound = findByName(model.getModel());
        if(modelFound == null){
            var modelName = UtilsString.capitalizeEachWord(model.getModel());
            model.setModel(modelName);

            return modelRepository.save(model);
        }
        return model;
    }

    private Model findByName(String name){
        return modelRepository.findByModelIgnoreCase(name);
    }

}
