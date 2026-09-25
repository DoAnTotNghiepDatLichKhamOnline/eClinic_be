package iuh.fit.fe.be_websatlichkham.common.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, Object id) {
        super(ErrorCode.RESOURCE_NOT_FOUND, resource + " không tồn tại (id = " + id + ")");
    }

}
