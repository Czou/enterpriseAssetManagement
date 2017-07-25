package com.jtj.web.service.impl;

import com.jtj.web.common.*;
import com.jtj.web.common.utils.MD5String;
import com.jtj.web.dao.PermissionDao;
import com.jtj.web.dao.PointDao;
import com.jtj.web.dao.UserDao;
import com.jtj.web.dto.PermissionDto;
import com.jtj.web.dto.UserDto;
import com.jtj.web.entity.Permission;
import com.jtj.web.entity.Point;
import com.jtj.web.entity.User;
import com.jtj.web.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;

/**
 * Created by jiang (jiang.taojie@foxmail.com)
 * 2016/12/23 23:26 End.
 */
@Service
public class UserServiceImpl
        extends BaseServiceImpl<User,UserDto,UserDao>
        implements UserService {

    @Autowired
    private UserDao userDao;
    @Autowired
    private PermissionDao permissionDao;
    @Autowired
    private PointDao pointDao;

    @Override
    public ResultDto<User> getUserById(long id){
        ResultDto<User> result = new ResultDto<>(ResultCode.SUCCESS);
        result.setObject(userDao.getUserById(id));
        return result;
    }

    @Override
    public ResultDto<Object> login(HttpServletRequest request, HttpServletResponse response,
                                   String name, String password,Long time) {
        ResultDto<Object> result = new ResultDto<>();
        //User user = userDao.login(name,password);
        User user = userDao.getUserByName(name);
        //不存在该用户
        if (user == null) {
            result.setResultCode(ResultCode.USER_INFO_NON_EXISTENT);
            result.setMessage("请重新检测帐号是否输错！");
            return result;
        }

        Long serverTime = new Date().getTime();
        Long timeInterval = serverTime -time;
        if (timeInterval > 10*60*1000 || timeInterval < -10*60*1000){
            result.setResultCode(ResultCode.USER_TIME_ERROR);
            result.setMessage("请校准时间后再次登录!");
            return result;
        }

        String MD5Password = MD5String.getMD5Str(user.getPassword() + time);
        if (!MD5Password.equals(password)){
            result.setResultCode(ResultCode.USER_PASSWORD_ERROR);
            return result;
        }

        //用户信息放入session
        user.setPassword("保密");
        HttpSession session = request.getSession();
        session.setAttribute(Constant.SESSION_USER,user);
        session.setAttribute(Constant.SESSION_LOGIN_TIME,serverTime);
        session.setAttribute(Constant.SESSION_PREVIOUS_PASSWORD,password);

        List<Permission> permissions = permissionDao.getByRoleId(user.getRoleId());
        session.setAttribute(Constant.SESSION_PERMISSION,permissions);

        List<Point> points = pointDao.getAuthorizedPoint(user.getId(),user.getRoleId());
        session.setAttribute(Constant.SESSION_POINT,points);

        result.setResultCode(ResultCode.SUCCESS);
        return result;
    }

    @Override
    public ResultDto<Object> logout(HttpServletRequest request, HttpServletResponse response) {
        ResultDto<Object> result = new ResultDto<>();
        //清除session
        HttpSession session = request.getSession();
        session.removeAttribute(Constant.SESSION_USER);
        result.setResultCode(ResultCode.SUCCESS);
        return result;
    }

    @Override
    public ResultDto<PageDto<User>> getList(UserDto dto) {
        ResultDto<PageDto<User>> result = new ResultDto<>(ResultCode.SUCCESS);

        //处理密码
        List<User> userList = userDao.getList(dto);
        userList.forEach(item -> item.setPassword("*******"));

        PageDto<User> page = new PageDto<>();
        page.setList(userList);
        page.setCount(userDao.getNum(dto));
        result.setObject(page);
        return result;
    }

    @Override
    public ResultDto<Object> updatePoint(Long id, Long pointId) {
        ResultDto<Object> result = new ResultDto<>();
        result.setResultCode(userDao.updatePoint(id,pointId)==1?ResultCode.SUCCESS:ResultCode.OPERATE_FAIL);
        return result;
    }

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        User user = userDao.getUserByName(s);
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        session.setAttribute(Constant.SESSION_USER,user);
        List<Permission> permissions = permissionDao.getByRoleId(user.getRoleId());
        session.setAttribute(Constant.SESSION_PERMISSION,permissions);
        List<Point> points = pointDao.getAuthorizedPoint(user.getId(),user.getRoleId());
        session.setAttribute(Constant.SESSION_POINT,points);
        return user;
    }
}
