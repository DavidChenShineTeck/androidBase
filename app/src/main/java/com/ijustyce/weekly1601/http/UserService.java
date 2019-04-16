package com.ijustyce.weekly1601.http;

import com.ijustyce.weekly1601.model.AlbumList;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * Created by yangchun on 2016/11/10.
 */

public interface UserService {

    @GET("api/1/phoneApi/v1/album")
    Call<AlbumList> listAlbum();
}
