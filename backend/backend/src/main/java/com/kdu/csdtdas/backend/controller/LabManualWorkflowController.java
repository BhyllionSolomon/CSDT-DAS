package com.kdu.csdtdas.backend.controller;
import com.kdu.csdtdas.backend.dto.LabManualWorkflowDTO;
import com.kdu.csdtdas.backend.service.LabManualWorkflowService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.List;
@RestController @RequestMapping("/api/lab/manual-workflow")
public class LabManualWorkflowController {
 private final LabManualWorkflowService service;
 public LabManualWorkflowController(LabManualWorkflowService s){service=s;}
 @PostMapping(value="/submissions",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public LabManualWorkflowDTO.Row upload(@RequestParam String title,@RequestParam Long courseId,@RequestParam Integer pageCount,@RequestParam(required=false) BigDecimal suggestedLevy,@RequestPart("file") MultipartFile file,Authentication a){return service.upload(a.getName(),role(a),title,courseId,pageCount,suggestedLevy,file);}
 @GetMapping("/mine") public List<LabManualWorkflowDTO.Row> mine(Authentication a){return service.mine(a.getName());}
 @GetMapping("/hod-queue") public List<LabManualWorkflowDTO.Row> hodQueue(Authentication a){return service.hodQueue(role(a));}
 @GetMapping("/lab-queue") public List<LabManualWorkflowDTO.Row> labQueue(Authentication a){return service.labQueue(role(a));}
 @PutMapping("/{id}/approve") public LabManualWorkflowDTO.Row approve(@PathVariable Long id,@RequestBody LabManualWorkflowDTO.Review r,Authentication a){return service.review(a.getName(),role(a),id,r,true);}
 @PutMapping("/{id}/return") public LabManualWorkflowDTO.Row sendBack(@PathVariable Long id,@RequestBody LabManualWorkflowDTO.Review r,Authentication a){return service.review(a.getName(),role(a),id,r,false);}
 @PutMapping("/{id}/dispatch") public LabManualWorkflowDTO.Row dispatch(@PathVariable Long id,@RequestBody LabManualWorkflowDTO.Dispatch d,Authentication a){return service.dispatch(a.getName(),role(a),id,d.targetLab());}
 @PutMapping("/{id}/receive") public LabManualWorkflowDTO.Row receive(@PathVariable Long id,Authentication a){return service.receive(a.getName(),role(a),id);}
 @GetMapping("/{id}/file") public ResponseEntity<byte[]> file(@PathVariable Long id,Authentication a){byte[] bytes=service.download(a.getName(),role(a),id);return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=\"manual-"+id+".pdf\"").body(bytes);}
 private String role(Authentication a){return a.getAuthorities().stream().map(x->x.getAuthority()).filter(x->x.startsWith("ROLE_")).map(x->x.substring(5)).findFirst().orElse("");}
}